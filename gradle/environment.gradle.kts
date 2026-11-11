fun configureEnvironment(task: ProcessForkOptions) {
    val f = File("${rootProject.projectDir}/properties.env")
    if (f.isFile) {
        f.readLines().forEachIndexed { index, line ->
            if (line.isNotEmpty() && !line.startsWith("#")) {
                val pair = line.split("=", limit = 2)
                if (pair.size != 2) {
                    project.logger.error("Error in properties.env on line ${index + 1}: '$line'")
                }
                task.environment[pair[0]] = pair[1]
            }
        }
    }
}
extra["configureEnvironment"] = { task: ProcessForkOptions -> configureEnvironment(task) }
