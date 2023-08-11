package model

import com.fractalwrench.json2kotlin.ConversionInfo

data class DataModel(val endpoint: Api.Endpoint, val model: ConversionInfo, val entity: Entity)
