const _CONFIG = {
    "tools": [
        {
            // The name is used for Headings in the web frontend.
            "name": "ULTIMATE Automizer",
            "id": "automizer",
            "description": "Verification of safety properties based on an automata-theoretic approach to software verification",
            "languages": ["Boogie", "C"],
            "workers": [
                {
                    "name": "c",
                    "id": "cAutomizer"
                },
                {
                    "name": "boogie",
                    "id": "boogieAutomizer"
                }
            ],
            "logo_url": "img/tool_logo.png"
        },
        {
            "name": "ULTIMATE Büchi Automizer",
            "id": "buechi_automizer",
            "description": "Termination analysis based on Büchi automata ",
            "languages": ["Boogie", "C"],
            "workers": [
                {
                    "name": "c",
                    "id": "cAutomizer"
                },
                {
                    "name": "boogie",
                    "id": "boogieAutomizer"
                }
            ]
        }
    ]
};
