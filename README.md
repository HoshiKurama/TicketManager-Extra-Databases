# ExtraDatabases
ExtraDB is a collection of official TicketManager Extensions which provide alternative database implementations for:
- MySQL
- H2 (pure; no memory caching).

## Installation & Setup
Place ExtraDB into the plugins folder. TicketManager 11+ will automatically select it during TM:SE initialization. Depending on which version was installed, setup instructions will vary.

### MySQL
Configuration is required prior to operation:
1. Run server. This will generate a configuration file at `plugins/TicketManager/addons/ExtraDatabases/MySQL/config.yml`. *Note: TicketManager will fail to load because ExtraDB failed to load.*
2. Provide values for the required fields in the configuration file
3. Restart server.

Alternatively, users may create the folder path described in step 1 and manually create/place the config file. This saves an extra server restart. Copy and paste the contents from [here](https://github.com/HoshiKurama/TicketManager-Extra-Databases/blob/main/Pure-MySQL/src/main/resources/config.yml) into your config file, then provide values for the required fields and start the server.

### H2
No configuration is required for operation. However, a configuration file generated at `plugins/TicketManager/addons/ExtraDatabases/H2/config.yml` contains settings which may be tweaked.
