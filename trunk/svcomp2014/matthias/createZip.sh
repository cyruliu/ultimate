#!/bin/bash
mkdir automizer
cp -a ../../source/BA_SiteRepository/target/products/CLI-E3/linux/gtk/x86_64/* automizer/
cp ../LICENSE* automizer/
cp ../../examples/toolchains/TraceAbstractionC.xml automizer/
cp ../../examples/settings/AutomizerSvcomp.settings automizer/
cp AutomizerSvcomp.py automizer/
zip UltimateCommandline.zip -r automizer/*