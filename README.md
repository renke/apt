APT – Analysis of Petri nets and labeled transition systems
===========================================================

![APT logo](doc/logo.png)

Welcome to APT. The purpose of this software is to run various analysis methods
on Petri nets and labeled transition systems. This guide explains the most
important aspects for getting started with APT.

This tool emerged of a student project group at the University of Oldenburg.

Participants of the project group:
Dennis Borde, Sören Dierkes, Raffaela Ferrari, Manuel Gieseking, Vincent Göbel, Renke Grunwald,
Björn von der Linde, Daniel Lückehe, Chris Schierholz, Uli Schlachter, Maike Schwammberger, V. Spreckels.

Recommended publications:
* General: [Eike Best, Uli Schlachter: Analysis of Petri Nets and Transition
  Systems](http://dx.doi.org/10.4204/EPTCS.189.6). In [ICE
  2015](http://dx.doi.org/10.4204/EPTCS.189): 53-67
* Synthesis: [Uli Schlachter: Petri Net Synthesis for Restricted Classes of
  Nets](http://dx.doi.org/10.1007/978-3-319-39086-4_6). In [Petri Nets
  2016](http://dx.doi.org/10.1007/978-3-319-39086-4): 79-97

Further reading
---------------

* [Obtaining APT](doc/obtaining.md)
* [Guide for using APT](doc/using.md)
* [JSON-interface for APT](doc/json.md)
* [The file format](doc/file_format.md)
* [Extending APT with own modules](doc/extending.md)
* [Javadoc API documentation](http://CvO-theory.github.io/apt-javadoc/)
* [Internal structure of APT](doc/internals.md)
* [A Graphical user interface for APT](https://github.com/CvO-Theory/apt-gui)
* [Benchmarking hints](doc/benchmarking.md)


Short guide for using APT
-------------------------

APT is a command line application. If you start it without any further
arguments, it provides a list of available modules:

    $ java -jar apt.jar
    Usage: apt <module> <arguments>

    Petri net
    =========
      bcf                          Check if a Petri net is behaviourally conflict free (BCF)
      bicf                         Check if a Petri net is binary conflict free (BiCF)
      bounded                      Check if a Petri net is bounded or k-bounded
      coverability_graph           Compute a Petri net's coverability graph
    [...]

If you provide the name of a module, you get information about how that module
is to be called:

    $ java -jar apt.jar bounded
    Too few arguments

    Usage: apt bounded <pn> [<k>]
      pn         The Petri net that should be examined
      k          If given, k-boundedness is checked

    Check if a Petri net is bounded or k-bounded. A Petri net is bounded if there
    is an upper limit for the number of token on each place. It is k-bounded if
    this limit isn't bigger than k.

This module can be used as follows:

    $ java -jar apt.jar  bounded nets/eb-nets/basic/pn3-net.apt
    bounded: No
    witness_place: s2
    witness_firing_sequence: "a;b"

If you have a file in e.g. the LoLA file format, you can call APT directly on
this file:

    $ java -jar apt.jar bounded lola:some_file.lola

The [guide for using APT](doc/using.md) provides more explanations and examples.
