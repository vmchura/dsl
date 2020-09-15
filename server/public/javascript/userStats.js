/**
 *
 * @param discordUserID: String
 * @param componentID:  Id del componente sin # inicial
 * @returns {{msg: string}}
 */
function buildUserStats(discordUserID, componentID) {
    var xhttp = new XMLHttpRequest();
    xhttp.onreadystatechange = function() {
        if (this.readyState == 4 && this.status == 200) {
            console.log(this.responseText);

            new Vue({
                el: "#"+componentID,
                data: () => ({
                        itemsPerPage: 4,
                        items: [
                            {
                                name: 'Frozen Yogurt',
                                calories: 159,
                                fat: 6.0,
                                carbs: 24,
                                protein: 4.0,
                                sodium: 87,
                                calcium: '14%',
                                iron: '1%',
                            },
                            {
                                name: 'Ice cream sandwich',
                                calories: 237,
                                fat: 9.0,
                                carbs: 37,
                                protein: 4.3,
                                sodium: 129,
                                calcium: '8%',
                                iron: '1%',
                            },
                            {
                                name: 'Eclair',
                                calories: 262,
                                fat: 16.0,
                                carbs: 23,
                                protein: 6.0,
                                sodium: 337,
                                calcium: '6%',
                                iron: '7%',
                            },
                            {
                                name: 'Cupcake',
                                calories: 305,
                                fat: 3.7,
                                carbs: 67,
                                protein: 4.3,
                                sodium: 413,
                                calcium: '3%',
                                iron: '8%',
                            },
                        ],
                    }),

            });
        }
    };
    xhttp.open("GET", "/userstats/"+discordUserID, true);
    xhttp.send();


}