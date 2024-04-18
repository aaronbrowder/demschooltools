from django.db import models


class Student(models.Model):
    class Meta:
        db_table = "students"

    id = models.IntegerField(db_column="_id", primary_key=True)
    person = models.ForeignKey(
        "dst.Person", db_column="dst_id", on_delete=models.PROTECT
    )


class Swipe(models.Model):
    class Meta:
        db_table = "swipes"

    id = models.IntegerField(db_column="_id", primary_key=True)
    student = models.ForeignKey(Student, on_delete=models.PROTECT)
    swipe_day = models.DateField()
