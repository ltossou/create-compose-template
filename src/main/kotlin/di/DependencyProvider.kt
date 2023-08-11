package di

import JsonConverter
import api.MapperGenerator
import api.NetworkModuleGenerator
import api.RemoteDataSourceGenerator
import api.RepositoryGenerator
import local.DatabaseGenerator
import local.DatabaseModuleGenerator
import local.LocalDataSourceGenerator
import java.nio.file.Path

class DependencyProvider(private val rootPackageName: String, val projectPath: Path) {
    val remoteDataSourceGenerator by lazy { RemoteDataSourceGenerator(projectPath, packageProvider) }
    val localDataSourceGenerator by lazy { LocalDataSourceGenerator(projectPath, packageProvider) }
    val repositoryGenerator by lazy { RepositoryGenerator(packageProvider) }
    val mapperGenerator by lazy { MapperGenerator(packageProvider) }
    val networkModuleGenerator by lazy { NetworkModuleGenerator(packageProvider) }
    val databaseModuleGenerator by lazy { DatabaseModuleGenerator(packageProvider) }
    val databaseGenerator: DatabaseGenerator by lazy { DatabaseGenerator(projectPath, packageProvider) }

    val packageProvider by lazy { PackageProvider(rootPackageName) }
    val jsonConverter: JsonConverter by lazy { JsonConverter() }
}