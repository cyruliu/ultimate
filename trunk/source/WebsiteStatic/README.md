**Ultimate** framework website.

# Configuration
Configuration is set via [config.js](js/config.js). The configuration consists of 2 sections:

**1. Backend:**
```
...
    backend: {
        web_bridge_url: 'URL to the WebsiteEclipseBridge server.'
    },
...
```

**2. Tools:**
```
...
    tools: [
        {
            name: "ULTIMATE Automizer",
            id: "automizer",
            description: "Verification of ...",
            languages: ["Boogie", "C"],
            workers: [
                {
                    name: "c",
                    id: "cAutomizer"
                },
                {
                    name: "boogie",
                    id: "boogieAutomizer"
                }
            ],
            logo_url: "img/tool_logo.png"
        },
        ...
```

# Todo: 
* [ ] Refactor the javascript code base.
* [ ] Evaluate and introduce new methods to provide settings to the toolchains.
* [ ] Refactor the API to use data Objects instead of long concatenated strings.
* [ ] Send and use asynchronous tasks to the backend WebsiteEclipseBridge.
* [ ] Update the dependencies.


# Dependencies
* [ace-editor](https://ace.c9.io/)
* [jquery](https://jquery.com/)
* [handlebars](https://handlebarsjs.com/)



# ULR parameters
The URL can contain up to two parameters:

```
/?ui={int|tool|home}?tool={automizer| ...} 

ui=home : show the landing page. (By default).
ui=tool : show informations about the tool.
ui=int  : show the interactive tool interface. (The editor).
```

