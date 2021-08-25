# ghidra2frida
[![](https://img.shields.io/github/stars/federicodotta/ghidra2frida.svg?color=yellow)](https://github.com/federicodotta/ghidra2frida)
[![](https://img.shields.io/github/forks/federicodotta/ghidra2frida.svg?color=green)](https://github.com/federicodotta/ghidra2frida)
[![](https://img.shields.io/github/issues-raw/federicodotta/ghidra2frida.svg?color=red)](https://github.com/federicodotta/ghidra2frida/issues)
[![](https://img.shields.io/badge/license-MIT%20License-red.svg?color=lightgray)](https://opensource.org/licenses/MIT) 
[![](https://img.shields.io/badge/twitter-apps3c-blue.svg)](https://twitter.com/apps3c)

ghidra2frida is a Ghidra Extension that, working as a bridge between [Ghidra](https://ghidra-sre.org/) and [Frida](https://www.frida.re/), lets you create powerful Ghidra scripts that take advantage of Frida's dynamic analysis engine to improve Ghidra statical analysis features. It supports all platforms supported by Frida (Windows, macOS, Linux, iOS, Android, and QNX).

The plugin is based on [Brida](https://github.com/federicodotta/Brida) idea (and code). ghidra2frida itself is a extension that adds to Ghidra a control panel with all the instruments necessary to create the bridge between Ghidra and Frida. When the bridge is up, a service is offered to Ghidra scripts and extensions that with a couple of lines of code can use dynamical instrumentation powerful features of Frida for everything you need.

Some examples:

1. Demangle SWIFT function names while analyzing iOS binaries (supplied as example)
2. Write a Ghidra analyzer that analyze a binary using also dynamic information obtained through Frida during one or more run of the binary itself
3. Decrypt encrypted portion of a binary (strings, etc.) calling the decryption function used by the target binary runing on target platform

A **tutorial** of the tool can be found in [our company blog](https://security.humanativaspa.it/ghidra2frida-the-new-bridge-between-ghidra-and-frida/).

# Requirements
In order to be able to use ghidra2frida, you need:
1.	Ghidra
2.	Frida client
3.	Pyro4
4.	An application to analyze! :D

# Installation from GitHub
1.	Install Python 2.7 or Python 3, Pyro4 (pip install pyro4) and frida (pip install frida). python virtual environments are fully supported.
2.	Download Ghidra: https://github.com/NationalSecurityAgency/ghidra/releases
3.	Download the last release of ghidra2frida: https://github.com/federicodotta/ghidra2frida/releases
4.	Open Ghidra -> File -> Install Extensions -> Click the "Add extension" button -> Choose ghidra2fridaXX.zip file
5.	Restart Ghidra

# Build

You can import the project in Eclipse using GhidraDev Eclipse plugin or you can manually build the plugin with gradle (I used gradle 7.1.1) as follows:
1. enter the project folder
2. export GHIDRA_INSTALL_DIR=*<PATH_GHIDRA_DIRECTORY>*
3. gradle
4. The compiled plugin is in the *dist* forlder
