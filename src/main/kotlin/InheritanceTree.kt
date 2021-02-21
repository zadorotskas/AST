class InheritanceTree {
    private class Node(val name: String, val isInterface: Boolean) {
        val children = mutableListOf<Node>()
    }

    private val depths = mutableListOf<Int>()
    private var maxDepth = -1
    private var avgDepth = -1.0
    private val root = Node("", false)


    fun addNode(nodeName: String, parents: Map<String, Boolean>, isInterface: Boolean) {
        val node = Node(name = nodeName, isInterface = isInterface)
        if (parents.isEmpty()) {
            if (findNode(root, nodeName, isInterface) == null) root.children.add(node)
            return
        }

        parents.forEach { parent ->
            val parentName = parent.key
            val parentIsInterface = parent.value
            var parentNode = findNode(root, parentName, parentIsInterface)
            if (parentNode == null) {
                parentNode = Node(parentName, parentIsInterface)
                root.children.add(parentNode)

                val toRemove = mutableListOf<Node>()
                root.children.forEach {
                    if (it.name == nodeName && it.isInterface == isInterface) {
                        toRemove.add(it)
                        //root.children.remove(it)
                    }
                }
                toRemove.forEach {
                    root.children.remove(it)
                }
            }

            parentNode.children.add(node)
        }
    }

    private fun findNode(parent: Node, nodeToFind: String, isInterface: Boolean): Node? {
        parent.children.forEach {
            if (it.name == nodeToFind && it.isInterface == isInterface) {
                return it
            }
            val node = findNode(it, nodeToFind, isInterface)
            if (node != null) return node
        }
        return null
    }

    fun getMaxDepth(): Int {
        if (maxDepth < 0) {
            maxDepth = findMaxDepth(root)
        }
        return maxDepth
    }

    fun getAvgDepth(): Double {
        if (avgDepth < 0) {
            findAvgDepth(root, 0)
            avgDepth = depths.fold(0) { res, number -> res + number }.toDouble() / depths.size
        }
        return avgDepth
    }

    private fun findMaxDepth(node: Node): Int =
        if (node.children.isEmpty()) 0
        else node.children.map { findMaxDepth(it) }.maxOrNull()!! + 1

    private fun findAvgDepth(node: Node, currentDepth: Int) {
        if (node.children.isEmpty()) {
            depths.add(currentDepth)
            return
        }
        node.children.forEach {
            findAvgDepth(it, currentDepth + 1)
        }
    }
}