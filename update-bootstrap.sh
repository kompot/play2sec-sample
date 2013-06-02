#!/bin/sh

if [ -z "$1" ]; then
  echo 'First and only param should be Twitter Bootstrap git branch name (e. g. `3.0.0-wip`)'
  exit 1
fi

#############################
# Warning! 3.0.0-wip breaks LESS compilation by Play!
# Some StackOverflow exceptions because of two places in Bootstrap.
#
# navs.less
#   .nav-divider {
#      .nav-divider();
#   }
#
# utilities.less
#   .clearfix {
#      .clearfix();
#   }
#
# Replaced these calls with inline code.
#############################

mkdir -p app/assets/stylesheets/tbs
cd app/assets/stylesheets/tbs
rm *.less
git clone https://github.com/twitter/bootstrap
cd bootstrap
git checkout $1
cd less
mv *.less ../../
cd ../../
rm -rf bootstrap
