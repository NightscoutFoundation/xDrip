#InfluxDB Grafana Dashboard

**InfluxDB support and the example Grafana diabetes dashboard was suggested and developed by: Julius de Bruijn (pimeys)**



> If you're able to write data to InfluxDB and have the latest Grafana communicating with the database, here's the dashboard[0] I've been able to build with the current data. To see all the panels you need Grafana 4.1, InfluxDB 1.x and the Grafana histogram[1] plugin. I have the glucose graph with alerts outside my target levels, a delta graph which is handy for seeing the current trend, a histogram to see how the different values are spread throughout the timespan and the current estimated GHbA1c.

> The dashboard needs some settings tweaks to work outside my own setup, but should be easy enough. The default retention policy[2] in InfluxDB is 7 days until it starts to delete old data. You might want to change it before losing anything important.

> Remember, if you run this on a public server, https+basic auth+fail2ban to block everybody failing to authenticate. I wouldn't trust the security of InfluxDB alone, usually it's not run on a public server.

> You can use mg/dl. The uploader sends both values, just select the value you want in the panel editor or edit the json.

[0] https://gist.github.com/pimeys/e3a99763aea756f8712abd5dbe19ebdf

[1] https://grafana.net/plugins/mtanda-histogram-panel

[2] https://docs.influxdata.com/influxdb/v1.2/guides/downsampling_and_retention/


_Still todo: Convert the panel to mg/dl and provide both or a switchable one_