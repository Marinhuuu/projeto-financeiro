// Configuração do Tailwind para Dark Mode baseado em classe
if (typeof tailwind !== 'undefined') {
    tailwind.config = {
        darkMode: 'class'
    };
}

function inicializarTema() {
    const temaSalvo = localStorage.getItem('theme');
    const prefereEscuro = window.matchMedia('(prefers-color-scheme: dark)').matches;

    if (temaSalvo === 'dark' || (!temaSalvo && prefereEscuro)) {
        document.documentElement.classList.add('dark');
    } else {
        document.documentElement.classList.remove('dark');
    }
}

function toggleDarkMode() {
    if (document.documentElement.classList.contains('dark')) {
        document.documentElement.classList.remove('dark');
        localStorage.setItem('theme', 'light');
    } else {
        document.documentElement.classList.add('dark');
        localStorage.setItem('theme', 'dark');
    }
}

// Executa imediatamente
inicializarTema();
