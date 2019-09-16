const _CONFIG = {
    backend: {
        web_bridge_url: 'https://monteverdi.informatik.uni-freiburg.de/tomcat/WebsiteEclipseBridge/if?callback=?'
    },
    tools: [
        {
            name: "ULTIMATE Automizer",
            id: "automizer",
            description: "Verification of safety properties based on an automata-theoretic approach to software verification",
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
        {
            name: "ULTIMATE Büchi Automizer",
            id: "buechi_automizer",
            description: "Termination analysis based on Büchi automata ",
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
            ]
        }
    ]
};
