This is the **Ultimate** framework website. It provides a front-end for the Ultimate framework web API implemented by WebBackend.
It needs access to a running WebBackend applications API to run Ultimate jobs.

# Configuration and setup.
All configuration is set in `config/config.js`.
* Copy [config/config.dist.js](config/config.dist.js) to `config/config.js`.
* Edit `config/config.js` to your needs. The `config/config.dist.js` file is commented to guide the configuration.

## Toolchain configuration.
For each worker in `config.tools.worker` a toolchain named `<worker.id>.xml` must be available in `config
/ultimate_toolchain_xmls`. This toolchain XML can be edited to alter the toolchain.

## Code Examples
Code examples can be copied before building with Maven using the [build_examples.py](build_examples.py) script in `trunk/source/WebsiteStatic/build_examples.py`. 
To add new examples edit the `tool_examples_map` dictionary in this file accordingly.

## Tool details page
Each tool is associated with a details page. To alter its content, edit the page matching the `tool_id` in the
[config/too_pages](config/tool_pages) folder.

## Home page contents
The content sections are determined by the files in [config/home_page](config/home_page).

# Development
**Expected result response example:**
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

## Configure theme & style
### Bootstrap theming
Edit [bootstrap_dev/scss/main.scss](bootstrap_dev/scss/main.scss) and then run `npm run css` to apply changes.

## Dependencies
* [ace-editor](https://ace.c9.io/)
* [jquery](https://jquery.com/)
* [handlebars](https://handlebarsjs.com/)
* [bootstrap](https://getbootstrap.com/)
