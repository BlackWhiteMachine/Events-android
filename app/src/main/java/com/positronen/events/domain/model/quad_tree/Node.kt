package com.positronen.events.domain.model.quad_tree

import com.positronen.events.utils.Logger
import java.util.*

sealed class Node<Type>(
    val levels: Int,
    val boundingBox: BoundingBox,
    val leafCapacity: Int
) {
    abstract val size: Int
    abstract fun insert(point: Point, data: Type): Node<Type>
    abstract fun remove(data: Type): Node<Type>
    abstract fun intersect(other: BoundingBox): List<Pair<Point, Type>>
    abstract fun warmMap(): List<Node<Type>>

    operator fun contains(point: Point): Boolean = boundingBox.contains(point)

    class LeafNode<Type> constructor(
        levels: Int,
        boundingBox: BoundingBox,
        leafCapacity: Int,
        private val points: MutableList<Pair<Point, Type>> = mutableListOf()
    ) : Node<Type>(levels, boundingBox, leafCapacity) {

        val id = UUID.randomUUID().toString()

        override val size: Int
            get() = points.size

        override fun insert(point: Point, data: Type): Node<Type> {
            require(point in boundingBox) { "$point is outside of $this" }

            return if (levels > 0 && points.size > leafCapacity) {
                split().insert(point, data)
            } else {
                points += point to data
                this
            }
        }

        override fun remove(data: Type): Node<Type> {
            points.removeAll { it.second == data }
            return this
        }

        override fun intersect(other: BoundingBox): List<Pair<Point, Type>> {
            return if (boundingBox.intersects(other)) {
                points.filter { it.first in other }
            } else {
                emptyList()
            }
        }

        override fun warmMap(): List<Node<Type>> {

            return listOf(this)
        }

        internal fun getPoints(): MutableList<Pair<Point, Type>> = points

        override fun toString(): String {
            return "LeafNode(levels=$levels, boundingBox=$boundingBox, points=$points)"
        }

        private fun split(): BranchNode<Type> {
            val (bottomLeft, topRight) = boundingBox
            val (x0, y0) = bottomLeft
            val (x1, y1) = topRight

            val centreX = (x1 - x0) / 2
            val centreY = (y1 - y0) / 2

            val nextLevel = levels - 1

            val branch = BranchNode<Type>(levels, boundingBox, leafCapacity,
                quadrant0 = LeafNode(
                    nextLevel,
                    BoundingBox(
                        bottomLeft = Point(x0 + centreX, y0 + centreY),
                        topRight = Point(x1, y1)
                    ),
                    leafCapacity
                ),
                quadrant1 = LeafNode(
                    nextLevel,
                    BoundingBox(
                        bottomLeft = Point(x0, y0 + centreY),
                        topRight = Point(x0 + centreX, y1)
                    ),
                    leafCapacity
                ),
                quadrant2 = LeafNode(
                    nextLevel,
                    BoundingBox(
                        bottomLeft = Point(x0, y0),
                        topRight = Point(x0 + centreX, y0 + centreY)
                    ),
                    leafCapacity
                ),
                quadrant3 = LeafNode(
                    nextLevel,
                    BoundingBox(
                        bottomLeft = Point(x0 + centreX, y0),
                        topRight = Point(x1, y0 + centreY)
                    ),
                    leafCapacity
                )
            )

            points.forEach { branch.insert(it.first, it.second) }

            return branch
        }
    }

    class BranchNode<Type>(
        levels: Int,
        boundingBox: BoundingBox,
        leafCapacity: Int,
        private var quadrant0: Node<Type>,
        private var quadrant1: Node<Type>,
        private var quadrant2: Node<Type>,
        private var quadrant3: Node<Type>
    ) : Node<Type>(levels, boundingBox, leafCapacity) {

        override val size: Int
            get() = quadrant0.size + quadrant1.size + quadrant2.size + quadrant3.size

        override fun insert(point: Point, data: Type): Node<Type> {
            require(point in this) { "$point is outside of $this" }

            if (point.y >= quadrant0.boundingBox.bottomLeft.y) {
                if (point.x >= quadrant0.boundingBox.bottomLeft.x) {
                    quadrant0 = quadrant0.insert(point, data)
                } else {
                    quadrant1 = quadrant1.insert(point, data)
                }
            } else {
                if (point.x >= quadrant3.boundingBox.bottomLeft.x) {
                    quadrant3 = quadrant3.insert(point, data)
                } else {
                    quadrant2 = quadrant2.insert(point, data)
                }
            }

            return this
        }

        override fun remove(data: Type): Node<Type> {
            quadrant0 = quadrant0.remove(data)
            quadrant1 = quadrant1.remove(data)
            quadrant2 = quadrant2.remove(data)
            quadrant3 = quadrant3.remove(data)

            return if (size > leafCapacity) {
                this
            } else {
                unite()
            }
        }

        override fun intersect(other: BoundingBox): List<Pair<Point, Type>> {
            return if (boundingBox.intersects(other)) {
                quadrant0.intersect(other) + quadrant1.intersect(other) + quadrant2.intersect(other) + quadrant3.intersect(other)
            } else {
                emptyList()
            }
        }

        override fun toString(): String {
            return "BranchNode(levels=$levels, boundingBox=$boundingBox)"
        }

        override fun warmMap(): List<Node<Type>> {
            return quadrant0.warmMap() + quadrant1.warmMap() + quadrant2.warmMap() + quadrant3.warmMap()
        }

        private fun unite(): LeafNode<Type> {
            val points = mutableListOf<Pair<Point, Type>>().apply {
                (quadrant0 as? LeafNode)?.getPoints()?.let {
                    addAll(it)
                }
                (quadrant1 as? LeafNode)?.getPoints()?.let {
                    addAll(it)
                }
                (quadrant2 as? LeafNode)?.getPoints()?.let {
                    addAll(it)
                }
                (quadrant3 as? LeafNode)?.getPoints()?.let {
                    addAll(it)
                }
            }
            return LeafNode(levels, boundingBox, leafCapacity, points)
        }
    }
}
