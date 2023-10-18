// ak maju modali spravne fungovat musia byt naviazane na body
// tento kod jednoducho naviaze modal na body
// modalID - definuje ID daneho modalu v HTML

export default {
    data() {
        return {
            modal: null,
        };
    },
    methods: {
        initModal(modalID) {
            this.modal = new bootstrap.Modal(document.getElementById(modalID), {
                keyboard: false,
                backdrop: 'static',
            });
            let body = document.getElementsByTagName('body');
            let modalHTML = document.getElementById(modalID);
            body[0].appendChild(modalHTML);
        },
        removeModal(modalID) {
            this.modal.hide();
            let modalHTML = document.getElementById(modalID);
            modalHTML.remove();
        },
    },
};
