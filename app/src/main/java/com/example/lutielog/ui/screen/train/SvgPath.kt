package com.example.lutielog.ui.screen.train

import androidx.compose.ui.graphics.Path

/**
 * 自包含的轻量 SVG path 解析器。
 *
 * 不依赖 Compose 的 PathParser（其对部分路径存在解析失败、返回空路径的问题），
 * 直接用 Path.moveTo/lineTo/cubicTo 等基础 API 构造，保证一定能渲染。
 *
 * 支持命令：M/m、L/l、H/h、V/v、C/c、S/s、Q/q、T/t、A/a（近似为线段）、Z/z，
 * 以及隐含重复命令。本项目动作库路径实际只用到了 m/M、l/L、c/C、z。
 */
object SvgPath {

    private val TOKEN = Regex("([MmLlHhVvCcSsQqTtAaZz])|(-?\\d*\\.?\\d+(?:[eE][-+]?\\d+)?)")

    /**
     * 把 SVG path 字符串解析为 Compose [Path]。
     * @param data SVG path data（虚拟坐标系）
     * @param offsetX 绘制时 x 方向的平移量（后视图用，归一化到 0..VIEW_W）
     */
    fun toCompose(data: String, offsetX: Float = 0f): Path {
        val path = Path()
        val tokens = TOKEN.findAll(data).map { it.value }.toList()
        var i = 0
        var cmd = ' '
        var x = 0f; var y = 0f
        var startX = 0f; var startY = 0f
        var lastCtlX = 0f; var lastCtlY = 0f
        var lastIsCubic = false
        var lastIsQuad = false

        fun num(): Float {
            if (i >= tokens.size) return 0f
            val v = tokens[i].toFloatOrNull() ?: 0f
            i++
            return v
        }

        fun moveto(nx: Float, ny: Float, rel: Boolean) {
            x = if (rel) x + nx else nx
            y = if (rel) y + ny else ny
            path.moveTo(x + offsetX, y)
            startX = x; startY = y
            lastIsCubic = false; lastIsQuad = false
        }

        fun lineto(nx: Float, ny: Float, rel: Boolean) {
            x = if (rel) x + nx else nx
            y = if (rel) y + ny else ny
            path.lineTo(x + offsetX, y)
            lastIsCubic = false; lastIsQuad = false
        }

        fun hline(nx: Float, rel: Boolean) {
            x = if (rel) x + nx else nx
            path.lineTo(x + offsetX, y)
            lastIsCubic = false; lastIsQuad = false
        }

        fun vline(ny: Float, rel: Boolean) {
            y = if (rel) y + ny else ny
            path.lineTo(x + offsetX, y)
            lastIsCubic = false; lastIsQuad = false
        }

        fun cubic(x1: Float, y1: Float, x2: Float, y2: Float, ex: Float, ey: Float, rel: Boolean) {
            val c1x = if (rel) x + x1 else x1
            val c1y = if (rel) y + y1 else y1
            val c2x = if (rel) x + x2 else x2
            val c2y = if (rel) y + y2 else y2
            val endX = if (rel) x + ex else ex
            val endY = if (rel) y + ey else ey
            path.cubicTo(c1x + offsetX, c1y, c2x + offsetX, c2y, endX + offsetX, endY)
            lastCtlX = c2x; lastCtlY = c2y
            x = endX; y = endY
            lastIsCubic = true; lastIsQuad = false
        }

        while (i < tokens.size) {
            val t = tokens[i]
            if (t.length == 1 && t[0] in "MmLlHhVvCcSsQqTtAaZz") {
                cmd = t[0]
                i++
            } else if (cmd == ' ') {
                break // 无命令直接结束，防御
            }

            val rel = cmd.isLowerCase()
            when (cmd.uppercaseChar()) {
                'M' -> {
                    val nx = num(); val ny = num()
                    moveto(nx, ny, rel)
                    cmd = if (rel) 'l' else 'L' // M 后省略命令按 L 处理
                }
                'L' -> lineto(num(), num(), rel)
                'H' -> hline(num(), rel)
                'V' -> vline(num(), rel)
                'C' -> {
                    val x1 = num(); val y1 = num(); val x2 = num(); val y2 = num(); val ex = num(); val ey = num()
                    cubic(x1, y1, x2, y2, ex, ey, rel)
                }
                'S' -> {
                    // 控制点1 = 上一 C/S 控制点2 的反射（无则取当前点）
                    val c1x = if (lastIsCubic) x + (x - lastCtlX) else x
                    val c1y = if (lastIsCubic) y + (y - lastCtlY) else y
                    val x2 = num(); val y2 = num(); val ex = num(); val ey = num()
                    val c2x = if (rel) x + x2 else x2
                    val c2y = if (rel) y + y2 else y2
                    val endX = if (rel) x + ex else ex
                    val endY = if (rel) y + ey else ey
                    path.cubicTo(c1x + offsetX, c1y, c2x + offsetX, c2y, endX + offsetX, endY)
                    lastCtlX = c2x; lastCtlY = c2y
                    x = endX; y = endY
                    lastIsCubic = true; lastIsQuad = false
                }
                'Q' -> {
                    val x1 = num(); val y1 = num(); val ex = num(); val ey = num()
                    val c1x = if (rel) x + x1 else x1
                    val c1y = if (rel) y + y1 else y1
                    val endX = if (rel) x + ex else ex
                    val endY = if (rel) y + ey else ey
                    path.quadraticTo(c1x + offsetX, c1y, endX + offsetX, endY)
                    lastCtlX = c1x; lastCtlY = c1y
                    x = endX; y = endY
                    lastIsQuad = true; lastIsCubic = false
                }
                'T' -> {
                    val c1x = if (lastIsQuad) x + (x - lastCtlX) else x
                    val c1y = if (lastIsQuad) y + (y - lastCtlY) else y
                    val ex = num(); val ey = num()
                    val endX = if (rel) x + ex else ex
                    val endY = if (rel) y + ey else ey
                    path.quadraticTo(c1x + offsetX, c1y, endX + offsetX, endY)
                    lastCtlX = c1x; lastCtlY = c1y
                    x = endX; y = endY
                    lastIsQuad = true; lastIsCubic = false
                }
                'A' -> {
                    // 本项目未用到弧线，近似为线段
                    num(); num(); num(); num(); num()
                    lineto(num(), num(), rel)
                }
                'Z' -> {
                    path.close()
                    x = startX; y = startY
                    lastIsCubic = false; lastIsQuad = false
                }
            }
        }
        return path
    }
}
