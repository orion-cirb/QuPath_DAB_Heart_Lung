# QuPath_DAB_Heart_Lung

* **Developed for:** Iris
* **Team:** Germain
* **Date:** April 2023
* **Software:** QuPath

### Images description

2D images of sections of pig heart and lungs taken with the Axioscan

1. *Hematoxylin* channel: tissue
2. *DAB* channel: red blood cells

### Plugin description

* Detect tissue in RGB channel with a pixel classifier
* In tissue, detect red blood cells in DAB channel with a pixel classifier
* Give red blood cells number and area in each annotation provided

### Dependencies

* **QuPath pixel classifiers** named *tissue.json* and *DAB.json*

### Version history

Version 1 released on April 13, 2022.
