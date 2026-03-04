#!/usr/bin/env bash
# Display an image inline using the iTerm2 inline image protocol (OSC 1337).
# Usage: ./imgcat.sh <image-file>

if [ $# -eq 0 ]; then
  echo "Usage: $0 <image-file>" >&2
  exit 1
fi

if [ ! -f "$1" ]; then
  echo "Error: File not found: $1" >&2
  exit 1
fi

BASE64=$(base64 < "$1")
printf "\033]1337;File=inline=1;width=auto;height=auto:%s\a" "$BASE64"
