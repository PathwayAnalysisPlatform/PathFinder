#!/usr/bin/env bash

##
# This script extracts the shortest paths from the available networks.
##


## Parameters

# Repository folder
repo=/mnt/work/marc/pap/github/PathFinder

# Execution parameters
nThreads=24


## Functions


## Script

# Compute the scores on the different cohorts
java -d64 -XX:MaxDirectMemorySize=60g -Xmx120G -cp $repo/bin/PathFinder-0.0.1/PathFinder-0.0.1.jar no.uib.pap.pathfinder.cmd.ExportShortestPathMatrix $nThreads
