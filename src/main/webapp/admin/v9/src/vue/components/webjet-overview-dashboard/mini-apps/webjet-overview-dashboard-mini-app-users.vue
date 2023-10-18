<template>
  <div :class="'overview-logged users'">
    <div class="overview-logged__head">
      <div class="overview-logged__head__icon">
        <i class="fas fa-user"></i>
      </div>
         <span>
            {{$loggedAdmins}}
         </span>
      <div class="overview-logged__head__more">
        <i class="aaafar f-a-ellipsis-v"></i>
      </div>
    </div>

    <div class="overview-logged__content">
      <ul>
        <li
            v-for="(user, index) in allUsers ? users : usersComputed"
            :key="index"
        >
          <a :href="user.link" :title="user.fullName">
            <img :src="user.photo">
            <span class="name" v-text="user.fullName"></span>
          </a>
        </li>
        <li class="show-more" v-if="!allUsers">
          <a @click.prevent="showAll">+{{remainingUsersLength}}</a>
        </li>
      </ul>
    </div>

  </div>
</template>

<script lang="js">

export default {
  name: 'webjet-overview-dashboard-mini-app-users',

  props: ['overviewadmins'],

  computed: {
    usersComputed: function() {
      let arr = [];
      if (typeof this.users != "undefined") {
        this.users.slice([0], [4]).map((item, i) => {
          arr.push(item);
        });
      }
      return arr;
    },
    remainingUsersLength: function() {
      if (typeof this.users == "undefined" || this.users.length < 5) {
        this.allUsers = true;
        return
      }
      return "+" + this.users.length - this.usersComputed.length
    },
  },

  data() {
    return {
      allUsers: false,
      users: this.$props.overviewadmins,
    }
  },

  methods: {
    showAll: function() {
      this.allUsers = true;
    }
  }
}

</script>

<style lang="scss">

</style>