#!/bin/bash
check () {
git fetch
UPSTREAM=${1:-'@{u}'}
LOCAL=$(git rev-parse @)
REMOTE=$(git rev-parse "$UPSTREAM")
BASE=$(git merge-base @ "$UPSTREAM")

if [ $LOCAL = $REMOTE ]; then
    echo "up-to-date"
elif [ $LOCAL = $BASE ]; then
    echo "Need to pull"
elif [ $REMOTE = $BASE ]; then
    echo "Need to push"
else
    echo "Diverged"
fi
}

wait_for_git_changes () {
    git_status="up-to-date"
    while [[ "$git_status" == "up-to-date" ]]; do
		echo "Sleeping for 5s"
        sleep 5s #Sleep for interval mins
        git_status=`check`
        echo "New git status is $git_status"
    done
}

wait_for_git_changes
