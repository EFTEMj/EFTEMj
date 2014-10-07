- [ ] Use `IJ.Prefs` at all plugins yet implemented.
- [ ] Add a documentation to all plugins by using `GenericDialog.addHelp(String url)`
- [ ] [DriftDetectionPlugin.java][drift]: Add an option to show a normalised copy of the stack. This will help to judge the result of the drift correction.
- [x] [DriftDetectionPlugin.java][drift]: Implement the manual drift correction.<br />I removed the selection of the detection mode from [DriftDetectionPlugin.java][drift]. If I implement a manual detection, this will be done as an independent plugin.
- [x] [ElementalMappingPlugin.java][map]: Implement new background fit methods (least squares & weighted least squares).<br />**Done**
- [x] [SR_EELS_CorrectionPlugin.java][sr-eels.d]: Complete the correction by adding the intensity correction and the dispersion correction.<br />I will implement a new correction (see next task).
- [x] Implement the new correction method that is based on the characterization [SR-EELS_characterisation.ijm][sr-eels.ijm].<br /> The implementation is finished, but I have to add additional Javadoc.
- [ ] Complete the tutorials ([Tutorial_Drift.java][tutorial.drift], [Tutorial_ESI.java][tutorial.esi],...).

[drift]: https://github.com/EFTEMj/EFTEMj/blob/master/EFTEMj/src/main/java/drift/DriftDetectionPlugin.java
[map]: https://github.com/EFTEMj/EFTEMj/blob/master/EFTEMj/src/main/java/elemental_map/ElementalMappingPlugin.java
[sr-eels.d]: https://github.com/EFTEMj/EFTEMj/blob/master/EFTEMj/src/main/java/sr_eels/deprecated/SR_EELS_CorrectionPlugin.java
[sr-eels.ijm]: https://github.com/EFTEMj/EFTEMj/blob/master/Scripts%2BMacros/SR-EELS_characterisation.ijm
[tutorial.drift]: https://github.com/EFTEMj/EFTEMj/blob/master/EFTEMj/src/main/java/tutorials/Tutorial_Drift.java
[tutorial.esi]: https://github.com/EFTEMj/EFTEMj/blob/master/EFTEMj/src/main/java/tutorials/Tutorial_ESI.java
