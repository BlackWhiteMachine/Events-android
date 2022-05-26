package com.positronen.events.domain.model.quad_tree

import com.positronen.events.utils.Logger
import java.util.*

sealed class Node<Type>(
    val capacity: Int,
    val levels: Int,
    val boundingBox: BoundingBox
) {
    abstract fun insert(point: Point, data: Type): Node<Type>
    abstract fun intersect(other: BoundingBox): List<Pair<Point, Type>>
    abstract fun warmMap(): List<Node<Type>>

    operator fun contains(point: Point): Boolean = boundingBox.contains(point)

    class LeafNode<Type>(
        capacity: Int,
        levels: Int,
        boundingBox: BoundingBox
    ) : Node<Type>(capacity, levels, boundingBox) {

        private val points = mutableListOf<Pair<Point, Type>>()

        val id = UUID.randomUUID().toString()

        override fun insert(point: Point, data: Type): Node<Type> {
            require(point in boundingBox) { "$point is outside of $this" }

            Logger.debug("MainActivity: LeafNode: $point")

            return if (levels > 0 && points.isNotEmpty()) {
                split().insert(point, data)
            } else {
                points += point to data
                this
            }
        }

        override fun intersect(other: BoundingBox): List<Pair<Point, Type>> {
            return if (boundingBox.intersects(other)) {
                points.filter { it.first in other }
            } else {
                emptyList()
            }
        }

        override fun warmMap(): List<Node<Type>> {

            Logger.debug("MainActivity: LeafNode: $this")

            return listOf(this)

//            return when {
//                points.isEmpty() -> emptyList()
//                points.size == 1 -> points
//                else -> {
//                    var resultX = 0f
//                    var resultY = 0f
//
//                    points.forEach {
//                        resultX += it.first.x/points.size
//                        resultY += it.first.y/points.size
//                    }
//
//                    listOf(Point(resultX, resultY) to points.first().second)
//                }
//            }
        }

        fun getPoints(): List<Pair<Point, Type>> = points

        private fun split(): BranchNode<Type> {
            val (bottomLeft, topRight) = boundingBox
            val (x0, y0) = bottomLeft
            val (x1, y1) = topRight

            val centreX = (x1 - x0) / 2
            val centreY = (y1 - y0) / 2

            val nextLevel = levels - 1

            val branch = BranchNode<Type>(capacity, levels, boundingBox,
                quadrant1 = LeafNode(capacity, nextLevel, BoundingBox(
                    bottomLeft = Point(x0 + centreX, y0 + centreY),
                    topRight = Point(x1, y1)
                )),
                quadrant2 = LeafNode(capacity, nextLevel, BoundingBox(
                    bottomLeft = Point(x0, y0 + centreY),
                    topRight = Point(x0 + centreX, y1)
                )),
                quadrant3 = LeafNode(capacity, nextLevel, BoundingBox(
                    bottomLeft = Point(x0, y0),
                    topRight = Point(x0 + centreX, y0 + centreY)
                )),
                quadrant4 = LeafNode(capacity, nextLevel, BoundingBox(
                    bottomLeft = Point(x0 + centreX, y0),
                    topRight = Point(x1, y0 + centreY)
                ))
            )

            points.forEach { branch.insert(it.first, it.second) }

            return branch
        }

        override fun toString(): String {
            return "LeafNode(capacity=$capacity, levels=$levels, boundingBox=$boundingBox, points=$points)"
        }
    }

    class BranchNode<Type>(
        capacity: Int,
        levels: Int,
        boundingBox: BoundingBox,
        private var quadrant1: Node<Type>,
        private var quadrant2: Node<Type>,
        private var quadrant3: Node<Type>,
        private var quadrant4: Node<Type>
    ) : Node<Type>(capacity, levels, boundingBox) {

        override fun insert(point: Point, data: Type): Node<Type> {
            require(point in this) { "$point is outside of $this" }

            Logger.debug("MainActivity: BranchNode: $point")

            if (point.y >= quadrant1.boundingBox.bottomLeft.y) {
                if (point.x >= quadrant1.boundingBox.bottomLeft.x) {
                    quadrant1 = quadrant1.insert(point, data)
                } else {
                    quadrant2 = quadrant2.insert(point, data)
                }
            } else {
                if (point.x >= quadrant4.boundingBox.bottomLeft.x) {
                    quadrant4 = quadrant4.insert(point, data)
                } else {
                    quadrant3 = quadrant3.insert(point, data)
                }
            }

            return this
        }

        override fun intersect(other: BoundingBox): List<Pair<Point, Type>> {
            return if (boundingBox.intersects(other)) {
                quadrant1.intersect(other) + quadrant2.intersect(other) + quadrant3.intersect(other) + quadrant4.intersect(other)
            } else {
                emptyList()
            }
        }

        override fun toString(): String {
            return "BranchNode(capacity=$capacity, levels=$levels, boundingBox=$boundingBox)"
        }

        override fun warmMap(): List<Node<Type>> {
            Logger.debug("MainActivity: BranchNode: $this")

            return quadrant1.warmMap() + quadrant2.warmMap() + quadrant3.warmMap() + quadrant4.warmMap()
        }
    }
}
