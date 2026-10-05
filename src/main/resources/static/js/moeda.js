/*
 * Máscara de moeda pt-BR (R$ 1.234,56) para campos <input data-moeda="idDoCampoOculto">.
 * O campo visível só exibe; o campo oculto (th:field) recebe o valor com ponto
 * decimal (ex.: 1234.56) para o binding do BigDecimal no Spring.
 */
(function () {

    function formatar(numero) {
        return numero.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
    }

    document.querySelectorAll('input[data-moeda]').forEach(function (exibicao) {

        var oculto = document.getElementById(exibicao.dataset.moeda);

        if (!oculto) {
            return;
        }

        // Valor já existente (ex.: tela de edição)
        var inicial = parseFloat(oculto.value);

        if (!isNaN(inicial)) {
            exibicao.value = formatar(inicial);
        }

        exibicao.addEventListener('input', function () {

            var digitos = exibicao.value.replace(/\D/g, '');

            if (!digitos) {
                exibicao.value = '';
                oculto.value = '';
                return;
            }

            var numero = parseInt(digitos, 10) / 100;

            exibicao.value = formatar(numero);
            oculto.value = numero.toFixed(2);
        });
    });
})();
