import com.google.gson.annotations.SerializedName

data class Metrics(
    @SerializedName("average_depth_of_inheritance") val avgDepth: Double,
    @SerializedName("maximum_depth_of_inheritance") val maxDepth: Int,
    @SerializedName("average_numbers_of_fields_in_classes") val avgNumberOfFields: Double,
    @SerializedName("average_numbers_of_overridden_methods") val avgNumbersOfOverriddenMethods: Double,
    @SerializedName("A") val aMetric: Int,
    @SerializedName("B") val bMetric: Int,
    @SerializedName("C") val cMetric: Int
)