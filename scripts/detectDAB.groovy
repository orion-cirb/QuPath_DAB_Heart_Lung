import qupath.lib.gui.dialogs.Dialogs
import static qupath.lib.scripting.QP.*
import qupath.lib.objects.classes.PathClass
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;


// Init project
def project = getProject()

if (!project.getPixelClassifiers().contains('tissue')) {
    Dialogs.showErrorMessage('Problem', 'No tissue classifier found')
    return
}
def tissueClassifier = project.getPixelClassifiers().get('tissue')
def tissueClass = PathClass.fromString('Tissue', makeRGB(150, 200, 200))

if (! project.getPixelClassifiers().contains('DAB')) {
    Dialogs.showErrorMessage('Problem', 'No DAB classifier found')
    return
}
def dabClassifier = project.getPixelClassifiers().get('DAB')
def dabClass = PathClass.fromString('DAB', makeRGB(0, 255, 0))

// Create results file and write headers
def imageDir = new File(project.getImageList()[0].getURIs()[0]).getParent()
def resultsDir = buildFilePath(imageDir, '/Results')
if (!fileExists(resultsDir)) mkdirs(resultsDir)
def resultsFile = new File(buildFilePath(resultsDir, 'detailedResults.xls'))
resultsFile.createNewFile()
resultsFile.write("Image name\tAnnotation name\tDAB object area (um2)\n")
def globalResultsFile = new File(buildFilePath(resultsDir, 'globalResults.xls'))
globalResultsFile.createNewFile()
globalResultsFile.write("Image name\tAnnotation name\tAnnotation area (um2)\tTissue area (um2)\tNb DAB objects\tDAB objects total area (um2)\tDAB objects mean area (um2)\tDAB objects area std\n")

// Save annotations
def saveAnnotations(imgName) {
    def path = buildFilePath(imgName + '.annot')
    def annotations = getAnnotationObjects()
    new File(path).withObjectOutputStream {
        it.writeObject(annotations)
    }
    println('Results saved')
}

// Get objects area statistics (sum, mean, std)
def getObjectsAreaStatistics(objects, pixelWidth) {
    def paramsValues = null
    if (objects.size() > 0) {
        def params = new DescriptiveStatistics()
        if (objects.size() > 0) {
            for (obj in objects) {
                params.addValue(obj.getROI().getScaledArea(pixelWidth, pixelWidth))
            }
        }
        paramsValues = [params.sum, params.mean, params.standardDeviation]
    } else {
        paramsValues = [0, 0, 0]
    }
    return paramsValues
}

// Loop over images in project
for (entry in project.getImageList()) {
    def imageData = entry.readImageData()
    def server = imageData.getServer()
    def cal = server.getPixelCalibration()
    def pixelWidth = cal.getPixelWidth().doubleValue()
    def pixelUnit = cal.getPixelWidthUnit()
    def imgName = entry.getImageName()
    setBatchProjectAndImage(project, imageData)
    setImageType('BRIGHTFIELD (H-DAB)')
    println ''
    println '------ ANALYZING IMAGE ' + imgName + ' ------'

    // Find annotations
    def annotations = getAnnotationObjects()
    if (annotations.isEmpty()) {
        Dialogs.showErrorMessage("Problem", "Please create annotations to analyze in image " + imgName)
        continue
    }
    def index = annotations.size()
    for (an in annotations) {
        if (an.getName() == null) {
            index++
            an.setName("Region_" + index)
        }
    }

    println '--- Finding tissue in image ---'
    deselectAll()
    selectObjects(annotations)
    createAnnotationsFromPixelClassifier(tissueClassifier, 0, 50)
    def tissues = getAnnotationObjects().findAll({it.getPathClass() == tissueClass})

    println '- Finding DAB objects in image -'
    deselectAll()
    selectObjects(tissues)
    createDetectionsFromPixelClassifier(dabClassifier, 1, 1, 'SPLIT')
    def dabCells = getDetectionObjects().findAll({it.getPathClass() == dabClass})

    for (an in annotations) {
        println '--- Saving results for annotation ' + an.getName() + ' ---'
        def tissue = tissues.find({it.getParent() == an})
        def cells = dabCells.findAll({it.getParent() == tissue})

        deselectAll()
        selectObjects(an)
        runPlugin('qupath.lib.plugins.objects.DilateAnnotationPlugin', '{"radiusMicrons": 0.1,  "lineCap": "Round",  "removeInterior": false,  "constrainToParent": false}');
        def anDil = getAnnotationObjects().last()
        removeObject(an, true)

        // Save annotation, tissue and DAB cells
        tissue.addChildObjects(cells)
        anDil.addChildObject(tissue)
        fireHierarchyUpdate()

        clearAllObjects()
        addObject(anDil)
        saveAnnotations(buildFilePath(resultsDir, imgName+"_"+anDil.getName()))

        // Write results in files
        for (cell in cells) {
            def results = imgName + "\t" + anDil.getName() + "\t" + cell.getROI().getScaledArea(pixelWidth, pixelWidth) + "\n"
            resultsFile << results
        }
        def cellsStatistics = getObjectsAreaStatistics(cells, pixelWidth)
        def globalResults = imgName + "\t" + anDil.getName() + "\t" + anDil.getROI().getScaledArea(pixelWidth, pixelWidth) + "\t" +
                            tissue.getROI().getScaledArea(pixelWidth, pixelWidth) + "\t" + cells.size() + "\t" + cellsStatistics[0] + "\t" +
                            cellsStatistics[1] + "\t" + cellsStatistics[2] + "\n"
        globalResultsFile << globalResults
    }
    clearAllObjects()
}
println '--- All done! ---'