/*
 * Disable install of the java.util.logging bridge for sl4j
 */
def sep = File.separator
def confFile = new File("grails-app${sep}conf${sep}Config.groovy")
def newConfCnt = (confFile.text =~ /(?m)^(grails\.logging\.jul\.usebridge\s*=\s*)true\s*$/).replaceAll("grails.logging.jul.usebridge = false")
confFile.write(newConfCnt);
