JAVAC = javac
JAVA = java
SRC_DIR = src/main/java
BIN = bin

CLASSES = $(shell find $(SRC_DIR) -name "*.java")

# Terminal emulator you use, adjust if needed
TERMINAL = alacritty

all: compile

compile:
	@echo "Compiling source files..."
	@mkdir -p $(BIN)
	$(JAVAC) -d $(BIN) $(CLASSES)
	@echo "Compilation done."

run-peers:
	@if [ -z "$(TERMINAL)" ]; then \
		echo "❌ TERMINAL not set in Makefile. Please set it to your terminal emulator."; \
		exit 1; \
	fi; \
	$(TERMINAL) -e sh -c "$(JAVA) -cp $(BIN) p2p.Main 3000; echo 'Press ENTER to exit...'; read" & \
	sleep 1; \
	$(TERMINAL) -e sh -c "$(JAVA) -cp $(BIN) p2p.Main 4000 localhost:3000; echo 'Press ENTER to exit...'; read" & \
	sleep 1; \
	$(TERMINAL) -e sh -c "$(JAVA) -cp $(BIN) p2p.Main 4001 localhost:3000; echo 'Press ENTER to exit...'; read" & \
	sleep 4; \
	$(TERMINAL) -e sh -c "$(JAVA) -cp $(BIN) p2p.Main 4002 localhost:4000; echo 'Press ENTER to exit...'; read" & \
	sleep 1; \
#	$(TERMINAL) -e sh -c "$(JAVA) -cp $(BIN) p2p.Main 4001 localhost:3000; echo 'Press ENTER to exit...'; read"
run-one:
	$(TERMINAL) -e sh -c "$(JAVA) -cp $(BIN) p2p.Main 4005 localhost:3000; echo 'Press ENTER to exit...'; read" &
