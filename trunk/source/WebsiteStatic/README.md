**Ultimate** framework website.

# Configuration
Configuration is set via [config.js](config/config.js). The configuration consists of the sections:

**1. Backend:**
```
...
    backend: {
        web_bridge_url: 'URL to the WebsiteEclipseBridge server.'
    },
...
```

**2. Editor:**
```
...
    editor: {
        init_code: '// Enter code here ...'
    },
...
```

**3. Tools:**
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
                    id: "cAutomizer",
                    frontend_settings: [
                      {
                        name: "Check for memory leak in main procedure",
                        id: "chck_main_mem_leak",
                        type: "bool",
                        default: true
                      },
                      ...
                    ]
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

**4. Code Examples:**
```
...
    code_examples: {
        c: [
          {
            name: 'CyclicBuffer.c',
            source: 'CyclicBuffer.c',
            assoc_workers: ["cAutomizer"]
          }
        ],
        boogie: [
          {
            name: 'GoannaDoubleFreeWithoutPoin',
            source: 'GoannaDoubleFreeWithoutPoin.boogie',
            assoc_workers: ["boogieAutomizer", "boogieBuchiAutomizer"]
          }
        ],
        ...
      }
...
```

# Todo: 
* [ ] Implement sample loading
* [ ] Implement range settings. 
* [ ] Refactor the API to use data Objects instead of long concatenated strings.
* [ ] Send and use asynchronous tasks to the backend WebsiteEclipseBridge.
* [ ] Refactor the results display.

# Dependencies
* [ace-editor](https://ace.c9.io/)
* [jquery](https://jquery.com/)
* [handlebars](https://handlebarsjs.com/)

# Documentation
## _CONFIG.context
Contains

* `_CONFIG.context.tool` The config of the tool selected by the user.
* `_CONFIG.context.worker` The config of the tools worker selected by the user.
* `_CONFIG.context.url` The url parameters.


## Frontend ULR parameters
The URL can contain up to two parameters:

```
/?ui={int|tool|home}?tool={automizer| ...} 

ui=home : show the landing page. (By default).
ui=tool : show informations about the tool.
ui=int  : show the interactive tool interface. (The editor).
```

