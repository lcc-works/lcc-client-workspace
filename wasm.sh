#!/bin/bash
# Build script for Eaglercraft WASM GC (Offline Download + Online)
# Shows live progress during the build

set -euo pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m' # No Color

# Configuration
WORKSPACE_DIR="/workspaces/lcc-client-workspace/lcc-client/ec-workspace"
JAVA_HOME="${JAVA_HOME:-/usr/local/sdkman/candidates/java/17.0.20-amzn}"
GRADLE_OPTS="${GRADLE_OPTS:--Xmx4G -Xms4G}"

# Build modes
BUILD_OFFLINE=true
BUILD_ONLINE=true
CLEAN_FIRST=false
VERBOSE=false

# Progress tracking
TOTAL_STEPS=0
CURRENT_STEP=0

print_header() {
    echo -e "\n${BOLD}${BLUE}========================================${NC}"
    echo -e "${BOLD}${BLUE}  $1${NC}"
    echo -e "${BOLD}${BLUE}========================================${NC}\n"
}

print_step() {
    CURRENT_STEP=$((CURRENT_STEP + 1))
    echo -e "${CYAN}[${CURRENT_STEP}/${TOTAL_STEPS}]${NC} ${BOLD}$1${NC}"
}

print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_progress() {
    local current=$1
    local total=$2
    local label=$3
    local percent=$((current * 100 / total))
    local filled=$((percent / 2))
    local empty=$((50 - filled))
    printf "\r${CYAN}[${NC}"
    printf "%${filled}s" | tr ' ' '█'
    printf "%${empty}s" | tr ' ' '░'
    printf "${CYAN}] ${BOLD}%3d%%${NC} %s" "$percent" "$label"
    if [ $current -eq $total ]; then
        echo ""
    fi
}

show_usage() {
    cat << EOF
Usage: $0 [OPTIONS]

Builds Eaglercraft WASM GC for both offline download and online deployment.

OPTIONS:
    --offline-only      Build only the offline download bundle
    --online-only       Build only the online (web server) deployment
    --clean             Clean build directory before building
    --verbose           Show verbose Gradle output
    --java-home PATH    Set JAVA_HOME (default: $JAVA_HOME)
    --help              Show this help message

EXAMPLES:
    $0                          # Build both offline and online (default)
    $0 --offline-only           # Build only offline bundle
    $0 --online-only --clean    # Clean build for online only
    $0 --verbose                # Build with verbose output

OUTPUT:
    Offline bundle: target_teavm_wasm_gc/javascript_dist/
    Online files:   target_teavm_wasm_gc/javascript/
EOF
}

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --offline-only)
            BUILD_OFFLINE=true
            BUILD_ONLINE=false
            shift
            ;;
        --online-only)
            BUILD_OFFLINE=false
            BUILD_ONLINE=true
            shift
            ;;
        --clean)
            CLEAN_FIRST=true
            shift
            ;;
        --verbose)
            VERBOSE=true
            shift
            ;;
        --java-home)
            JAVA_HOME="$2"
            shift 2
            ;;
        --help)
            show_usage
            exit 0
            ;;
        *)
            print_error "Unknown option: $1"
            show_usage
            exit 1
            ;;
    esac
done

# Validate JAVA_HOME
if [ ! -d "$JAVA_HOME" ]; then
    print_error "JAVA_HOME not found: $JAVA_HOME"
    print_info "Available Java installations:"
    ls -la /usr/local/sdkman/candidates/java/ 2>/dev/null || true
    exit 1
fi

export JAVA_HOME
export PATH="$JAVA_HOME/bin:$PATH"
export GRADLE_OPTS="$GRADLE_OPTS"
export _JAVA_OPTIONS="$GRADLE_OPTS"

print_header "Eaglercraft WASM GC Build Script"
print_info "JAVA_HOME: $JAVA_HOME"
print_info "Workspace: $WORKSPACE_DIR"
print_info "Build Offline: $BUILD_OFFLINE"
print_info "Build Online:  $BUILD_ONLINE"
print_info "Clean First:   $CLEAN_FIRST"
print_info "Gradle Opts:  $GRADLE_OPTS"

# Calculate total steps
TOTAL_STEPS=0
if [ "$CLEAN_FIRST" = true ]; then
    TOTAL_STEPS=$((TOTAL_STEPS + 1))
fi
# Java compile + resources
TOTAL_STEPS=$((TOTAL_STEPS + 1))
# TeaVM WASM GC compilation
TOTAL_STEPS=$((TOTAL_STEPS + 1))
# Runtime JS compilation (Closure Compiler)
TOTAL_STEPS=$((TOTAL_STEPS + 1))
# EPK compilation (assets)
TOTAL_STEPS=$((TOTAL_STEPS + 1))
# Language EPK compilation
TOTAL_STEPS=$((TOTAL_STEPS + 1))
if [ "$BUILD_OFFLINE" = true ]; then
    # Bootstrap compilation + Client Bundle
    TOTAL_STEPS=$((TOTAL_STEPS + 2))
fi

print_info "Total build steps: $TOTAL_STEPS"

cd "$WORKSPACE_DIR"

# Ensure gradlew is executable
chmod +x gradlew

# Build gradle command with explicit JVM settings
GRADLE_CMD="./gradlew --no-daemon -Dorg.gradle.java.home=$JAVA_HOME"
if [ "$VERBOSE" = false ]; then
    GRADLE_CMD="$GRADLE_CMD -q"
fi

run_gradle() {
    local task=$1
    local label=$2
    
    print_step "$label"
    
    if [ "$VERBOSE" = true ]; then
        $GRADLE_CMD $task
    else
        # Run with --info to get progress, filter for relevant lines
        # Using a simpler approach: run gradle and filter output in real-time
        local tmpfile=$(mktemp)
        # Use exec to avoid subshell issues
        $GRADLE_CMD --info $task >"$tmpfile" 2>&1 &
        local pid=$!
        
        local last_line_count=0
        while kill -0 $pid 2>/dev/null; do
            if [ -f "$tmpfile" ]; then
                local current_line_count=$(wc -l < "$tmpfile")
                if [ $current_line_count -gt $last_line_count ]; then
                    # Show new lines
                    sed -n "$((last_line_count + 1)),$current_line_count p" "$tmpfile" | while IFS= read -r line; do
                        if [[ "$line" =~ ^(> Task|> Building|BUILD|Downloading|Extracting|Compiling|Processing|Generating|Assembling|Copying|Linking|:target_teavm_wasm_gc:) ]]; then
                            echo -e "  ${BLUE}▶${NC} $line"
                        elif [[ "$line" =~ (FAILED|ERROR|Exception|FAILURE) ]]; then
                            echo -e "  ${RED}✗${NC} $line"
                        elif [[ "$line" =~ (SUCCESSFUL|UP-TO-DATE) ]]; then
                            echo -e "  ${GREEN}✓${NC} $line"
                        fi
                    done
                    last_line_count=$current_line_count
                fi
            fi
            sleep 3
        done
        
        wait $pid
        local status=$?
        
        # Show final output (last 30 lines)
        if [ -f "$tmpfile" ]; then
            tail -30 "$tmpfile" | while IFS= read -r line; do
                if [[ "$line" =~ ^(> Task|> Building|BUILD|Downloading|Extracting|Compiling|Processing|Generating|Assembling|Copying|Linking|:target_teavm_wasm_gc:) ]]; then
                    echo -e "  ${BLUE}▶${NC} $line"
                elif [[ "$line" =~ (FAILED|ERROR|Exception|FAILURE) ]]; then
                    echo -e "  ${RED}✗${NC} $line"
                elif [[ "$line" =~ (SUCCESSFUL|UP-TO-DATE) ]]; then
                    echo -e "  ${GREEN}✓${NC} $line"
                fi
            done
        fi
        rm -f "$tmpfile"
        
        if [ $status -ne 0 ]; then
            print_error "Task '$task' failed with exit code $status"
            exit $status
        fi
    fi
    
    print_success "$label completed"
}

# Step 1: Clean if requested
if [ "$CLEAN_FIRST" = true ]; then
    run_gradle "target_teavm_wasm_gc:clean" "Cleaning build directory"
fi

# Step 2: Compile Java sources
run_gradle "target_teavm_wasm_gc:compileJava target_teavm_wasm_gc:processResources" "Compiling Java sources & processing resources"

# Step 3: TeaVM WASM GC compilation (the heavy lifting)
run_gradle "target_teavm_wasm_gc:buildWasmGC" "Compiling to WASM GC (TeaVM)"

# Step 4: Compile WASM Runtime JavaScript (Closure Compiler)
run_gradle "target_teavm_wasm_gc:compileMainWasmRuntime" "Compiling WASM Runtime JavaScript (Closure Compiler)"

# Step 5: Compile Main EPK (assets)
run_gradle "target_teavm_wasm_gc:compileMainEpk" "Compiling main assets EPK"

# Step 6: Compile Language EPK
run_gradle "target_teavm_wasm_gc:compileMainLanguageEpk" "Compiling language assets EPK"

# Step 7: Build Offline Download Bundle
if [ "$BUILD_OFFLINE" = true ]; then
    # Compile bootstrap loader first
    run_gradle "target_teavm_wasm_gc:compileMainWasmBootstrap" "Compiling WASM bootstrap loader"
    
    # Build the complete client bundle (offline download)
    run_gradle "target_teavm_wasm_gc:makeMainWasmClientBundle" "Building offline download bundle"
fi

print_header "Build Complete!"

print_success "All requested builds completed successfully!"
echo ""
print_info "Output locations:"

if [ "$BUILD_ONLINE" = true ]; then
    echo -e "  ${CYAN}Online (Web Server)${NC}: ${WORKSPACE_DIR}/target_teavm_wasm_gc/javascript/"
    echo -e "    - classes.wasm (WASM binary)"
    echo -e "    - classes.wasm-runtime.js (WASM runtime)"
    echo -e "    - eagruntime.js (JavaScript runtime)"
    echo -e "    - loader.js, index.html, etc."
    echo -e "    - assets.epk (game assets)"
    echo -e "    - lang.tmp.epk (language assets)"
fi

if [ "$BUILD_OFFLINE" = true ]; then
    echo -e "  ${CYAN}Offline Download${NC}: ${WORKSPACE_DIR}/target_teavm_wasm_gc/javascript_dist/"
    echo -e "    - Complete self-contained client bundle"
    echo -e "    - bootstrap.js (offline loader)"
    echo -e "    - All assets bundled"
fi

echo ""
print_info "To serve online version locally:"
echo -e "  ${YELLOW}cd ${WORKSPACE_DIR}/target_teavm_wasm_gc/javascript && python3 -m http.server 8080${NC}"
echo ""
