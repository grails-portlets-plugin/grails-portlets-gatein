//
// This script is executed by Grails after plugin was installed to project.
// This script is a Gant script so you can use all special variables provided
// by Gant (such as 'baseDir' which points on project base dir). You can
// use 'ant' to access a global instance of AntBuilder
//
// For example you can create directory under project tree:
//
//    ant.mkdir(dir:"${basedir}/grails-app/jobs")
//

/*
* Disable install of the java.util.logging bridge for sl4j
*/
def sep = File.separator
def confFile = new File("grails-app${sep}conf${sep}Config.groovy")
def newConfCnt = (confFile.text =~ /(?m)^(grails\.logging\.jul\.usebridge\s*=\s*)true\s*$/).replaceAll("grails.logging.jul.usebridge = false")
confFile.write(newConfCnt);