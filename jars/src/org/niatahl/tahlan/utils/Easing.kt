package org.niatahl.tahlan.utils

/**
 * Taken from <a href="https://github.com/mattdesl/cisc226game/blob/master/SpaceGame/src/space/engine/easing/Easing.java">Github</a>
 *
 * @author Robert Penner (functions)
 * @author davedes (java port)
 * @author Wisp (kotlin port)
 */
object Easing {
    object Quadratic {
        /**
         * Quadratic easing in - accelerating from zero velocity.
         */
        fun easeIn(time: Float, valueAtStart: Float, valueAtEnd: Float, duration: Float): Float {
            return flipIfNeeded(time, valueAtStart, valueAtEnd, duration) { thyme, start, end, dur ->
                var t = thyme
                return@flipIfNeeded end * (run { t /= dur;t }) * t + start
            }
        }

        /**
         * Quadratic easing out - decelerating to zero velocity.
         */
        fun easeOut(time: Float, valueAtStart: Float, valueAtEnd: Float, duration: Float): Float {
            return flipIfNeeded(time, valueAtStart, valueAtEnd, duration) { thyme, start, end, dur ->
                var t = thyme
                return@flipIfNeeded -end * (run { t /= dur;t }) * (t - 2) + start
            }
        }

        /**
         * Quadratic easing in/out - acceleration until halfway, then deceleration
         */
        fun easeInThenOut(time: Float, valueAtStart: Float, valueAtEnd: Float, duration: Float): Float {
            return flipIfNeeded(time, valueAtStart, valueAtEnd, duration) { thyme, start, end, dur ->
                var t = thyme
                return@flipIfNeeded if ((run { t /= dur / 2;t }) < 1)
                    end / 2 * t * t + start;
                else -end / 2 * ((--t) * (t - 2) - 1) + start;
            }
        }
    }

    object Linear {
        /**
         * Simple linear tweening - no easing.
         */
        fun tween(time: Float, valueAtStart: Float, valueAtEnd: Float, duration: Float): Float {
            return flipIfNeeded(time, valueAtStart, valueAtEnd, duration) { thyme, start, end, dur ->
                return@flipIfNeeded end * thyme / dur + start
            }
        }
    }

    object Cubic {
        /**
         * Cubic easing in - accelerating from zero velocity.
         */
        fun easeIn(time: Float, valueAtStart: Float, valueAtEnd: Float, duration: Float): Float {
            return flipIfNeeded(time, valueAtStart, valueAtEnd, duration) { thyme, start, end, dur ->
                var t = thyme
                return@flipIfNeeded end * (run { t /= dur;t }) * t * t + start
            }
        }

        /**
         * Cubic easing out - decelerating to zero velocity.
         */
        fun easeOut(time: Float, valueAtStart: Float, valueAtEnd: Float, duration: Float): Float {
            return flipIfNeeded(time, valueAtStart, valueAtEnd, duration) { thyme, start, end, dur ->
                var t = thyme
                return@flipIfNeeded end * ((run { t = t / dur - 1;t }) * t * t + 1) + start;
            }
        }

        /**
         * Cubic easing in/out - acceleration until halfway, then deceleration
         */
        fun easeInThenOut(time: Float, valueAtStart: Float, valueAtEnd: Float, duration: Float): Float {
            return flipIfNeeded(time, valueAtStart, valueAtEnd, duration) { thyme, end, start, dur ->
                var t = thyme
                return@flipIfNeeded if ((run { t /= dur / 2;t }) < 1)
                    end / 2 * t * t * t + start
                else end / 2 * ((run { t -= 2;t }) * t * t + 2) + start;
            }
        }
    }

    private fun flipIfNeeded(
        time: Float,
        valueAtStart: Float,
        valueAtEnd: Float,
        duration: Float,
        easeFunction: (time: Float, valueAtStart: Float, valueAtEnd: Float, duration: Float) -> Float
    ): Float {
        return if (valueAtStart > valueAtEnd) {
            valueAtEnd - easeFunction(time, valueAtEnd, valueAtStart, duration)
        } else {
            easeFunction(time, valueAtStart, valueAtEnd, duration)
        }
    }
}