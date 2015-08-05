# Workflow

This document describes the workflows for analising data with EFTEMj.

## SR-EELS

There are two kinds of workflow related to SR-EELS

1. Characterisation of the energy filter parameters
2. correction of SR-EELS data sets

### Characterisation of the energy filter parameters

To find the best set off parameters, one has to investigate the influance off all the parameters. For this task a workflow was created that imports the recorded data to a kind of database (file based). From this database one can select data sets for comparison (plotting). Filters are used to find the data sets of interest.

#### Import characterisation measurements

A description of the characterisation measurements can be found at [EFTEMj@MC2015].

[SR-EELS_ImportCharacterisation.js] is the first script. It assumes that all images of a characterisation measurement are in a seperate folder. The folder name (or the mames of the parant folders) should contain the used parameters. The script can extract them by searching for [regular expressions].

1. The user has to select the folder that contains the images of a characterisation measurement.
1. A file selection dialog is presented. The script tries to select the correct files.
1. The script shows a prefilled dialog to state the parameters of the measurement. The user can change and amend them.
1. All selected images are imported to the database. They are rotated and converted to the TIFF file format.

#### Analyse the characterisation measurements

...


[EFTEMj@MC2015]: https://github.com/EFTEMj/EFTEMj/tree/master/MC2015
[SR-EELS_ImportCharacterisation.js]: https://github.com/EFTEMj/EFTEMj/blob/master/EFTEMj/src/main/resources/macros/SR-EELS_ImportCharacterisation.js
[regular expressions]: https://en.wikipedia.org/wiki/Regular_expression
