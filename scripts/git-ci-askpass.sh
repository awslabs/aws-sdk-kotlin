#!/bin/bash

# invoked by git when credentials are needed
# Note: will be invoked twice, once for the user, and then again for the password:
# example:
# git-askpass.sh 'Username for '\''https://github.com'\'': '
# git-askpass.sh 'Password for '\''https://USER@github.com'\'': '
if grep -q "Username" <<< "$1"; then
    echo "$CI_USER"
else
    echo "$CI_ACCESS_TOKEN"
fi
