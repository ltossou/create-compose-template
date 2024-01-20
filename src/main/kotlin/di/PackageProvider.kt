package di

import model.Api

class PackageProvider(val root: String) {
    fun dataLocalBase() = "$root.data.local.base"
    fun dataLocalRoot() = "$root.data.local"

    fun entities(apiName: String) = "$root.data.local.entity.$apiName".lowercase()

    fun daos(apiName: String) = "$root.data.local.dao.$apiName".lowercase()

    fun mappers(apiName: String) = "$root.data.local.mapper.$apiName".lowercase()

    fun di() = "$root.di"

    fun repositories(apiName: String) = "$root.data.repository.$apiName".lowercase()

    fun apis(apiName: String) = "$root.data.api.$apiName".lowercase()
    fun apis(api: Api) = apis(api.name.lowercase())

    fun apiModels(apiName: String) = "$root.data.api.$apiName.model".lowercase()

    fun fontTypography() = "$root.ui.theme".lowercase()
}