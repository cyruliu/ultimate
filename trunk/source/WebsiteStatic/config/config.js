const _CONFIG = {
  backend: {
    web_bridge_url: 'https://monteverdi.informatik.uni-freiburg.de/tomcat/WebsiteEclipseBridge/if?callback=?'
  },
  editor: {
    init_code: '// Enter code here ...'
  },
  tools: [
    {
      name: "ULTIMATE Automizer",
      id: "automizer",
      description: "Verification of safety properties based on an automata-theoretic approach to software verification",
      languages: ["Boogie", "C"],
      workers: [
        {
          language: "c",
          id: "cAutomizer",
          task_id: "AUTOMIZER_C",
          frontend_settings: [
            {
              name: "Check for memory leak in main procedure",
              id: "chck_main_mem_leak",
              type: "bool",
              default: true
            },
            {
              name: "Check for overflows of signed integers",
              id: "chck_signed_int_overflow",
              type: "bool",
              default: true
            }
          ]
        },
        {
          language: "boogie",
          id: "boogieAutomizer",
          task_id: "AUTOMIZER_BOOGIE",
          frontend_settings: [
            {
              name: "Check foo",
              id: "chck_foo",
              type: "bool",
              default: true
            },
            {
              name: "Check bar",
              id: "chck_bar",
              type: "bool",
              default: false
            }
          ]
        }
      ],
      logo_url: "img/tool_logo.png",
    },
    {
      name: "ULTIMATE Büchi Automizer",
      id: "buechi_automizer",
      description: "Termination analysis based on Büchi automata ",
      languages: ["Boogie", "C"],
      workers: [
        {
          language: "c",
          id: "cBuchiAutomizer",
          task_id: "TERMINATION_C",
          frontend_settings: []
        },
        {
          language: "boogie",
          id: "boogieBuchiAutomizer",
          task_id: "TERMINATION_BOOGIE",
          frontend_settings: []
        }
      ]
    }
  ],
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
    ]
  }
};
