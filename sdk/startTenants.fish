#!/usr/bin/env fish
# start_faircache.fish
# Launches both tenant instances in split tmux panes and sends Enter to both
# as close to simultaneously as possible.
#
# Usage (from your project root, where .env.tenant1 and .env.tenant2 live):
#   chmod +x start_faircache.fish
#   ./start_faircache.fish
 
set JAR "target/sdk-0.1-SNAPSHOT-jar-with-dependencies.jar"
set CMD1 "java -Denv.file=.env.tenant1 -Dtenant.id=1 -jar $JAR"
set CMD2 "java -Denv.file=.env.tenant2 -Dtenant.id=2 -jar $JAR"
 
# Kill any existing session with the same name
tmux kill-session -t faircache 2>/dev/null
 
# Create new detached session — left pane gets CMD1
tmux new-session  -d -s faircache -x 220 -y 50
 
# Split vertically — right pane
tmux split-window -h -t faircache
 
# Type (but don't execute) both commands
tmux send-keys -t faircache:0.0 $CMD1 ""
tmux send-keys -t faircache:0.1 $CMD2 ""
 
# Fire both Enter back-to-back — as close to simultaneous as fish can manage
tmux send-keys -t faircache:0.0 Enter
tmux send-keys -t faircache:0.1 Enter
 
# Attach so you can watch both panes
tmux attach -t faircache
 