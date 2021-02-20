package Array

fun findMaxConsecutiveOnes(nums: IntArray): Int {
	var maxCount = 0
	var count = 0
	val length = nums.size
	for (i in 0 until length) {
		if (nums[i] == 1) {
			count++
		} else {
			maxCount = maxCount.coerceAtLeast(count)
			count = 0
		}
	}
	maxCount = maxCount.coerceAtLeast(count)
	return maxCount
}

fun main(args: Array<String>) {
	val nums: IntArray = intArrayOf(1, 1, 0, 1, 1, 1)
	System.out.println("MaxConsecutiveOnes = ${findMaxConsecutiveOnes(nums)}")
}