# Deprecated - use [m-entrup/EFTEMj] instead!

I decided to clean up EFTEMj before releasing it at an [update-site](http://sites.imagej.net/) for [ImageJ][ij]/[Fiji][fiji]. The result is the new repository [m-entrup/EFTEMj].

# EFTEMj

Processing of Energy Filtering TEM images with ImageJ (Fiji)

## What is EFTEMj?

EFTEMj is a plugin for [Fiji][fiji] that combines a drift correction and a flexible elemental mapping routine. It has been created in 2009 as a part of a diploma thesis. The current version is missing some elemental mapping features, but it is extended by tools for spacialy resolved electron energy loss spectroscopy (SR-EELS). SR-EELS is the main subject of my PhD thesis.

## LICENSING

EFTEMj is distributed under a Simplified BSD License; for the full text of the license, see [LICENSE.txt](https://github.com/EFTEMj/EFTEMj/blob/master/LICENSE.txt).

## Why do we use Fiji (ImageJ)?

[ImageJ][ij] is an open source image processing program and one can access the full source code. Further advantages of ImageJ are the support for a wide range of file formats, the easy to learn macro language and the possibility to write complex plugins with Java. [Fiji][fiji] is a package that contains ImageJ and lots of plugins. The main feature of Fiji is the [automatic update function][updater].

## Drift correction

Due to the specimen drift in a TEM it is necessary to apply a drift correction to the recorded images before creating an elemental map. The used drift correction is based on the calculation of the normalized cross-correlation coefficient. The implementation is based on the code published by [Wilhelm Burger and Mark J. Burge][1], but it is extended for parallel computing.

## Elemental mapping

An introduction to elemental mapping with Energy Filtering Transmission Electron Microscopy (EFTEM) is given by [Ferdinand Hofer and Othmar Leitner][2] (no longer available). EFTEMj uses the Maximum Likelihood Estimation (MLE) [Unser1987] to estimate the background signal, but is is possible to switch the least squares and Levenberg–Marquardt algorithm. Additionally, more than two images can be used to estimate the background, which results in a higher reliability [[Heil2012]].

## Spatially resolved electron energy loss spectroscopy (SR-EELS)

SR-EELS is a method that preserves spatial information when recording [EEL spectra][eels] [[Reimer1988]]. The SR-EELS plugins included in EFTEMj are used to correct distortions that occur when applying SR-EELS with an in-column imaging energy filter of a Zeiss Libra 200FE.

For more information you can have a look at my [presentation][slides] created with [impress.js][impress].

## References

- [[Heil2012]] T. Heil et al., Ultramicroscopy **118** (2012), 11-16
- [Unser1987] M. Unser et al., Journal of Microscopy **145** (1987), 245-256
- [[Reimer1988]] L. Reimer et al., Ultramicroscopy **24** (1988) 339-354.

[eels]: https://en.wikipedia.org/wiki/Electron_energy_loss_spectroscopy
[ij]: http://imagej.net
[fiji]: http://imagej.net/Fiji
[updater]: http://imagej.net/Updater
[1]: http://www.imagingbook.com/
[2]: http://www.electroiq.com/articles/sst/print/volume-43/issue-3/features/metrology-test/metrology-eftem-provides-elemental-mapping-at-nanometer-resolution.html
[Heil2012]: http://dx.doi.org/10.1016/j.ultramic.2012.04.009
[Reimer1988]: http://dx.doi.org/10.1016/0304-3991%2888%2990126-X 
[slides]: https://eftemj.github.io
[impress]: https://github.com/bartaz/impress.js
[m-entrup/EFTEMj]: https://github.com/m-entrup/EFTEMj
