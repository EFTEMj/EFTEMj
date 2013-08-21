# EFTEMj

Processing of Energy Filtering TEM images with ImageJ

## What is EFTEMj?

EFTEMj is a plugin for [ImageJ][ij] that combines a drift correction and a flexible elemental mapping routine. It has been created in 2009 as a part of a diploma thesis. Until now (April the 11th, 2013) EFTEMj has not been finished, but it is planed to publish the source code under an open source license.

## LICENSING

EFTEMj is distributed under a Simplified BSD License; for the full text of the license, see [LICENSE.txt](https://github.com/EFTEMj/EFTEMj/blob/master/LICENSE.txt).

## Why do we use ImageJ?

ImageJ is an open source image processing program and one can access the full source code. Further advantages of ImageJ are the support for a wide range of file formats, the easy to learn macro language and the possibility to write complex plugins with Java.

## Drift correction

Due to the specimen drift in a TEM it is necessary to apply a drift correction to the recorded images before creating an elemental map. The used drift correction is based on the calculation of the normalized cross-correlation coefficient. The implementation is based on the code published by [Wilhelm Burger and Mark J. Burge][1], but it is extended for parallel computing.

## Elemental mapping

An introduction to elemental mapping with Energy Filtering Transmission Electron Microscopy (EFTEM) is given by [Ferdinand Hofer and Othmar Leitner][2]. EFTEMj uses the Maximum Likelihood Estimation (MLE) [Unser1987] to estimate the background signal. Additionally, more than two images can be used to estimate the background, which results in a higher reliability [[Heil2012]].

## Spatially resolved electron energy loss spectroscopy (SR-EELS)

SR-EELS is a method that preserves spatial information when recording EEL spectra [[Reimer1988]]. The SR-EELS plugins included in EFTEMj are used to correct distortions that occur when applying SR-EELS with an in-column imaging energy filter of a Zeiss Libra 200FE.

## References

- [[Heil2012]] T. Heil et al., Ultramicroscopy **118** (2012), 11-16
- [Unser1987] M. Unser et al., Journal of Microscopy **145** (1987), 245-256
- [Reimer1988] L. Reimer et al., Ultramicroscopy **24** (1988) 339-354.

[ij]: http://rsbweb.nih.gov/ij/
[1]: http://www.imagingbook.com/
[2]: http://www.electroiq.com/articles/sst/print/volume-43/issue-3/features/metrology-test/metrology-eftem-provides-elemental-mapping-at-nanometer-resolution.html
[Heil2012]: http://dx.doi.org/10.1016/j.ultramic.2012.04.009
[Reimer1988]: http://dx.doi.org/10.1016/0304-3991%2888%2990126-X 
