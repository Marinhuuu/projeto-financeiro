// Tema do MyFin para o Tailwind (CDN). Carregado por todas as telas logo após o script do Tailwind.
// "slate" ganha um tom levemente azulado (fundos, textos e bordas) e "indigo", a cor da marca, fica mais viva.
tailwind.config = {
    darkMode: 'class',
    theme: {
        extend: {
            colors: {
                slate: {
                    50: '#f5f7fc',
                    100: '#eceff8',
                    200: '#dfe3f0',
                    300: '#c5cbdf',
                    400: '#959db8',
                    500: '#687090',
                    600: '#4d5474',
                    700: '#3a4061',
                    800: '#242a47',
                    900: '#161b34',
                    950: '#0c1024'
                },
                indigo: {
                    50: '#f1f1ff',
                    100: '#e4e4ff',
                    200: '#cdccff',
                    300: '#aba7ff',
                    400: '#8b82fc',
                    500: '#7063f6',
                    600: '#5f4beb',
                    700: '#503ccf',
                    800: '#4233a7',
                    900: '#382f84',
                    950: '#221c4d'
                }
            },
            fontFamily: {
                sans: ['Inter', 'ui-sans-serif', 'system-ui', 'sans-serif']
            }
        }
    }
};
