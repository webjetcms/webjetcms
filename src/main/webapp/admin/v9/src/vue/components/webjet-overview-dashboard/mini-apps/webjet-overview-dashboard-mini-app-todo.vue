<template>
  <div :class="'overview-logged todolist'">
    <div class="overview-logged__head">
      <div class="overview-logged__head__icon">
        <i class="fas fa-clipboard-list"></i>
      </div>
      <span> ToDo </span>
      <div class="overview-logged__head__more">
        <i class="far fa-ellipsis-v"></i>
      </div>
    </div>

    <div class="overview-logged__content">
      <ul>
        <li v-for="(todo, index) in todolist" :key="index">
          <span class="date">{{
            $WJ.formatDateTimeSeconds(todo.createDate)
          }}</span>
          <span class="todoitem" v-text="todo.text"></span>
        </li>
      </ul>
    </div>
  </div>
</template>

<script lang="js">

export default {
  name: 'webjet-overview-dashboard-mini-app-todo',
  props: ['overviewtodo'],
  data() {
    return {
      todolist: this.$props.overviewtodo,
    }
  },
}
</script>

<style lang="scss">
.overview-logged {
  &.todolist {
    .overview-logged__head__icon {
      background: #6486ff;
    }
  }
  &__content {
    ul li {
      margin-bottom: 15px;
    }
  }
}

.overview-logged.todolist {
  ul {
    li {
      padding-left: 0px;
    }
  }
}
</style>
