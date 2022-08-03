<a href="https://www.bernardi.cloud/">
    <img src=".readme-files/photo-logo-72.png" alt="Photo logo" title="Photo" align="right" height="72" />
</a>

# Photo
> A graphical tool to organize your (my!😉) family photo library

[![Go](https://img.shields.io/badge/Go-v1.15-blue.svg)](https://golang.org/)
[![License](https://img.shields.io/github/license/bernarpa/jphoto.svg)](https://opensource.org/licenses/GPL-3.0)
[![GitHub issues](https://img.shields.io/github/issues/bernarpa/jphoto.svg)](https://github.com/bernarpa/jphoto/issues)

## Table of contents

- [What is JPhoto](#what-is-jphoto)
- [License](#license)

## What is JPhoto

JPhoto, aka BERNACON Photo Organizer, is a graphical Java Swing tool that we use to organize our family photographs.

JPhoto use is quite simple, you just need to select a directory that contains
your family photo collection and a directory where you will put newly added
photos (e.g. photos copied from your phone). The big "Organize" button classifies
the photos to be imported as follows:

1. **NoExif**: photos without exif data.
2. **Duplicates**: photos already present in the family photo library.
3. **Organized**: photos that can be added to the library, grouped in daily folders.

JPhoto is a GUI tool that I created to manage my photo library according to my established *modus operandi*.

JPhoto is a Java-based tool, with a Maven build configuration. I've developed it on a system with Java 15 and Apache Netbeans 13.

# License

JPhoto is licensed under the terms of the GNU General Public License version 3.
