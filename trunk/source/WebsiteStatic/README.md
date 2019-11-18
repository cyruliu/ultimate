**Ultimate** framework website.

# Configuration
Configuration is set via [config.js](config/config.js). The configuration consists of the sections:

**0. Meta:**
```
...
meta: {
    debug_mode: false,  # if set to true, `test/result.json` will be used as a response for fetching ultimate results.
}
...
```


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
Code examples are generated using the [build_examples.py](build_examples.py) script. 
To add new examples edit the `tool_examples_map` dict accordingly.

# Development
## Webbridge API
To fetch results from the ultimate tool for the interactive part, a POST request is sent to the URL defined in the
config section `backend.web_bridge_url`.

**POST request data:** (see `tool_interface.get_execute_settings()`)
```json
{
    "action": "execute",
    "code": "the code in the editor",
    "toolchain": {
      "id": _CONFIG.context.current_worker.id,
      "task_id": _CONFIG.context.current_worker.task_id,
    },
    "user_settings": {
      // The settings derrived from config `worker.frontend_settings` and set by the user.
    }
  }
```

**Result response example:**
```json
"results": [
    {
      "endCol": -1,
      "endLNr": -1,
      "logLvl": "warning",
      "longDesc": "unknown boogie variable #StackHeapBarrier",
      "shortDesc": "Unfinished Backtranslation",
      "startCol": -1,
      "startLNr": -1,
      "type": "warning"
    },
    ...
]
```

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

## Configure theme & style
### Bootstrap theming
Edit [bootstrap_dev/scss/main.scss](bootstrap_dev/scss/main.scss) and then run `npm run css` to apply changes.


# Dependencies
* [ace-editor](https://ace.c9.io/)
* [jquery](https://jquery.com/)
* [handlebars](https://handlebarsjs.com/)
* [bootstrap](https://getbootstrap.com/)
