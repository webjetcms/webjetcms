// jednoducho funkcie na ovladanie ref errov
// pouzivaju sa aj vo feedbackju a v bookmark preto vytiahnute ako mixin

export default {
    methods: {
        showError(errorRef) {
            this.$refs[errorRef].classList.remove('invisible');
        },
        hideError(errorRef) {
            console.log(this.$refs[errorRef]);
            this.$refs[errorRef].classList.value = this.$refs[errorRef].classList.value + ' invisible';
        },
    },
};
