package com.positronen.events.domain.model.quad_tree

class QuadTree<Type>(
    topRightX: Float,
    topRightY: Float,
    bottomLeftX: Float,
    bottomLeftY: Float,
    capacity: Int = 200,
    levels: Int = 2
) {

    private val boundingBox = BoundingBox(
        bottomLeft = Point(bottomLeftX, bottomLeftY),
        topRight = Point(topRightX, topRightY)
    )

    private var root: Node<Type> = Node.LeafNode(capacity, levels, boundingBox)

    fun insert(x: Float, y: Float, data: Type) {
        root = root.insert(Point(x, y), data)
    }

    fun intersect(
        topRightX: Float,
        topRightY: Float,
        bottomLeftX: Float,
        bottomLeftY: Float
    ): List<Type> {
        val boundingBox = BoundingBox(
            bottomLeft = Point(bottomLeftX, bottomLeftY),
            topRight = Point(topRightX, topRightY)
        )

        return root.intersect(boundingBox).map { it.second }
    }

    fun warmMap(): List<Node.LeafNode<Type>> {
        return root.warmMap().mapNotNull { it as? Node.LeafNode }
    }

    override fun toString(): String {
        return "QuadTree(root=$root)"
    }
}