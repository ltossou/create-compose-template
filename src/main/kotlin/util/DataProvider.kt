package util

import model.Api
import common.ResourceFileReader

object DataProvider {

    val fileReader = ResourceFileReader()

    object API {
        val WMATA_METRO = Api(
            name = "metro",
            baseUrl = "api.wmata.com",
            endpoints = listOf(
                Api.Endpoint(
                    method = Api.Endpoint.Method.GET,
                    url = "/stations",
                    json = fileReader.readContents("json/wmata.stations.json")
                )
            ),
            authentication = Api.AuthType.Header(key = "appId", value= "WMATA_KEY")
        )

        val SEATGEEK = Api(
            name = "seatgeek",
            baseUrl = "api.seatgeek.com/2",
            endpoints = listOf(
                Api.Endpoint(
                    method = Api.Endpoint.Method.GET,
                    url = "events",
                    json = fileReader.readContents("json/seatgeek.events.json")
                )
            ),
            authentication = Api.AuthType.Query(key = "client_id", value= "WMATA_KEY")
        )
    }
}