#!/usr/bin/env bash

all: clean BitTrust

clean:
    rm -rf out/production/BitTrust

BitTrust:
    mkdir -p out/production/BitTrust
    javac -cp ../peersim-1.0.5/*:../commons-math3-3.5/commons-math3-3.5.jar \ src/peersim/bittorrent/*.java src/utils/Interaction.java -d out/production/BitTrust
